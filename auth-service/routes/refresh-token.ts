import { UserInfoResponse } from '@/generated/user';
import { createBadRequiestError } from '@/utils/exceptions';
import { createMeta4ServiceRequest, setJwtCookies, TokenType } from '@/utils/jwt-utils';
import { FastifyInstance } from 'fastify';

export default async function (fastify: FastifyInstance) {
  fastify.post('/refresh', {}, async (request, reply) => {
    const { refresh_token: token } = request.cookies;
    if (!token) {
      throw createBadRequiestError("Refresh token not setted");
    }

    const info = fastify.jwt.getTokenInfo(token);
    if (info.type !== TokenType.REFRESH || !info.hasPayload) {
      console.info(`Wrong token: ${info.type !== TokenType.REFRESH ?
        `wrong type (${info.type})`
        : 'payload not provided'}`);
      throw createBadRequiestError("Wrong token");
    }
    if (info.expired) {
      console.info(`Expired token for ${info.id}`);
      throw createBadRequiestError("Expired token");
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

    console.info(`Success refresh token for ${info.id}`);

    setJwtCookies(fastify, reply, user, 'access');

    return reply.send({
      data: { success: true },
      message: 'Access token update successful',
    });
  });
}
