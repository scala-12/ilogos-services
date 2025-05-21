import { UserInfoResponse } from '@/generated/user';
import { createBadRequiestError, createUnauthorizedError } from '@/utils/exceptions';
import { createMeta4ServiceRequest, TokenType } from '@/utils/jwt-utils';
import { FastifyInstance } from 'fastify';

export default async function (fastify: FastifyInstance) {
  fastify.post('/verify', {}, async (request, reply) => {
    const { access_token: token } = request.cookies;
    if (!token) {
      throw createBadRequiestError("Access token not setted");
    }

    const info = fastify.jwt.getTokenInfo(token);
    if (info.type !== TokenType.ACCESS || !info.hasPayload) {
      console.info(`Wrong token: ${info.type !== TokenType.ACCESS ?
        `wrong type (${info.type})`
        : 'payload not provided'}`);
      throw createUnauthorizedError("Wrong token");
    }
    if (info.expired) {
      console.info(`Expired token for ${info.id}`);
      throw createUnauthorizedError("Expired token");
    }

    const metadata = createMeta4ServiceRequest(fastify);

    const user = await new Promise<UserInfoResponse>((resolve, reject) => {
      console.debug(`Request to user-service (${info.id})`);
      fastify.userGrpc.findUserById({ id: info.id }, metadata, (error, response) => {
        if (error) {
          console.debug(`Response from user-service: error (${info.id}, ${error})`);
          reject(error);
        } else {
          console.debug(`Response from user-service: success (${info.id})`);
          resolve(response);
        }
      });
    });

    if (user.email === info.email && user.username === info.username) {
      const userRoles = new Set(user.roles);
      if (new Set(info.roles).size === userRoles.size && info.roles.every(e => userRoles.has(e))) {
        console.info(`Success access token for ${info.id} (${info.username})`);
        return reply.send({
          data: { success: true },
          message: 'Access token verified',
        });
      } else {
        console.info(`Invalid token: roles (${info.id}, ${info.username})`);
      }
    } else {
      console.info(`Invalid token: username/email (${info.id}, ${info.username}/${info.email} != ${user.username}/${user.email})`);
    }

    throw createUnauthorizedError("Token invalid");
  });
}
