import { UserInfoResponse } from '@/generated/user';
import { TokenType } from '@/plugins/jwt';
import { createBadRequiestError } from '@/utils/exceptions';
import { createMeta4ServiceRequest, setJwtCookies } from '@/utils/jwt-utils';
import { FastifyInstance } from 'fastify';

export default async function (fastify: FastifyInstance) {
  fastify.post('/refresh', {}, async (request, reply) => {
    const { refresh_token: token } = request.cookies;
    if (!token) {
      throw createBadRequiestError("Refresh token not setted");
    }

    const info = fastify.jwt.getTokenInfo(token);
    if (info.type !== TokenType.REFRESH || !info.hasPayload) {
      throw createBadRequiestError("Wrong token");
    }
    if (info.expired) {
      throw createBadRequiestError("Expired token");
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

    setJwtCookies(fastify, reply, user, 'access');

    return reply.send({
      data: { success: true },
      message: 'Access token update successful',
    });
  });
}
