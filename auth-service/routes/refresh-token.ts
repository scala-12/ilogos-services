import { UserInfoResponse } from '@/generated/user';
import { createBadRequiestError } from '@/utils/exceptions';
import { createMeta4ServiceRequest, setJwtCookies } from '@/utils/jwt-utils';
import { FastifyInstance } from 'fastify';

export default async function (fastify: FastifyInstance) {
  fastify.post('/refresh', {}, async (request, reply) => {
    const { refresh_token: token } = request.cookies;
    if (!token) {
      throw createBadRequiestError("Refresh token not setted");
    }

    let info;
    try {
      info = fastify.jwt.getTokenInfo(token)
    } catch (err) {
      console.error("JWT token invalid");
      throw err;
    }

    if (!info.isRefreshToken || !info.hasPayload) {
      console.info(`Wrong token: ${!info.isRefreshToken ?
        `wrong type (${info.type})`
        : 'payload not provided'}`);
      throw createBadRequiestError("Wrong token");
    }
    if (info.isExpired) {
      console.info(`Expired token for ${info.subject}`);
      throw createBadRequiestError("Expired token");
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

    console.info(`Success refresh token for ${info.subject}`);

    setJwtCookies(fastify, reply, user, 'access');

    return reply.send({
      data: { success: true },
      message: 'Access token update successful',
    });
  });
}
