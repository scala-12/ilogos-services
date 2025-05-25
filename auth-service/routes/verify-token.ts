import { UserInfoResponse } from '@/generated/user';
import { createBadRequiestError, createUnauthorizedError } from '@/utils/exceptions';
import { createMeta4ServiceRequest } from '@/utils/jwt-utils';
import { FastifyInstance } from 'fastify';

export default async function (fastify: FastifyInstance) {
  fastify.post('/verify', {}, async (request, reply) => {
    const { access_token: token } = request.cookies;
    if (!token) {
      throw createBadRequiestError("Access token not setted");
    }

    let info;
    try {
      info = fastify.jwt.getTokenInfo(token)
    } catch (err) {
      console.error("JWT token invalid");
      throw err;
    }
    if (!info.isAccessToken || !info.hasPayload) {
      console.info(`Wrong token: ${!info.isAccessToken ?
        `wrong type (${info.type})`
        : 'payload not provided'}`);
      throw createUnauthorizedError("Wrong token");
    }
    if (info.isExpired) {
      console.info(`Expired token for ${info.subject}`);
      throw createUnauthorizedError("Expired token");
    }

    const metadata = createMeta4ServiceRequest(fastify);

    const user = await new Promise<UserInfoResponse>((resolve, reject) => {
      console.debug(`Request to user-service (${info.subject})`);
      fastify.userGrpc.findUserById({ id: info.subject }, metadata, (error, response) => {
        if (error) {
          console.debug(`Response from user-service: error (${info.subject}, ${error})`);
          reject(error);
        } else {
          console.debug(`Response from user-service: success (${info.subject})`);
          resolve(response);
        }
      });
    });

    if (info.checkUserData(user.username, user.email, user.roles)) {
      console.info(`Success access token for ${info.subject} (${info.username})`);
      return reply.send({
        data: { success: true },
        message: 'Access token verified',
      });
    } else {
      console.info(`Invalid token: username/email/roles (${info.subject}, ${info.username}/${info.email}/${info.roles} != ${user.username}/${user.email}/${user.roles})`);
    }

    throw createUnauthorizedError("Token invalid");
  });
}
