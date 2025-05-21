import { UserInfoResponse } from '@/generated/user';
import { TokenType } from '@/plugins/jwt';
import { createMeta4ServiceRequest } from '@/utils';
import { createBadRequiestError, createUnauthorizedError } from '@/utils/exceptions';
import { FastifyInstance } from 'fastify';

export default async function (fastify: FastifyInstance) {
  fastify.post('/verify', {}, async (request, reply) => {
    const { access_token: token } = request.cookies;
    if (!token) {
      throw createBadRequiestError("Access token not setted");
    }

    const info = fastify.jwt.getTokenInfo(token);
    if (info.type !== TokenType.ACCESS || !info.hasPayload) {
      throw createUnauthorizedError("Wrong token");
    }
    if (info.expired) {
      throw createUnauthorizedError("Expired token");
    }

    const metadata = createMeta4ServiceRequest(fastify);

    const user = await new Promise<UserInfoResponse>((resolve, reject) => {
      fastify.userGrpc.findUserById({ id: info.id }, metadata, (error, response) => {
        if (error) {
          reject(error);
        }
        resolve(response);
      });
    });

    if (user.email === info.email && user.username === info.username) {
      const userRoles = new Set(user.roles);
      if (new Set(info.roles).size === userRoles.size && info.roles.every(e => userRoles.has(e))) {
        return reply.send({
          data: { success: true },
          message: 'Access token verified',
        });
      }
    }

    throw createUnauthorizedError("Token invalid");
  });
}
