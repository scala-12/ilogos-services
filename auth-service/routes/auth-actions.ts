import { UserInfoResponse } from '@/generated/user';
import { prepareString } from '@/utils';
import { createUnauthorizedError } from '@/utils/exceptions';
import { clearJwtCookies, createMeta4ServiceRequest, setJwtCookies } from '@/utils/jwt-utils';
import { Static, Type } from '@sinclair/typebox';
import bcrypt from 'bcrypt';
import { FastifyInstance } from 'fastify';

const AuthForm = Type.Object({
  username: Type.Optional(Type.String()),
  email: Type.Optional(Type.String({ format: 'email' })),
  password: Type.String({ minLength: 6 })
})

export default async function (fastify: FastifyInstance) {
  fastify.post<{
    Body: Static<typeof AuthForm>
  }>('/login', {
    schema: {
      body: AuthForm
    }
  }, async (request, reply) => {
    let { username, email, password } = request.body;

    password = prepareString(password) || '';
    if (!password) {
      throw createUnauthorizedError('Password not provided');
    }
    const usernameOrEmail = prepareString(username, email);
    if (!usernameOrEmail) {
      throw createUnauthorizedError('Username not provided');
    }

    const metadata = createMeta4ServiceRequest(fastify);
    const user = await new Promise<UserInfoResponse>((resolve, reject) => {
      console.debug(`Request to user-service (${usernameOrEmail})`);
      fastify.userGrpc.findUserByEmailOrUsername({ usernameOrEmail }, metadata, (error, response) => {
        if (error) {
          console.debug(`Response from user-service: error (${usernameOrEmail}, ${error})`);
          reject(error);
        } else {
          console.debug(`Response from user-service: success (${usernameOrEmail})`);
          resolve(response);
        }
      });
    });

    const passwordValid = await bcrypt.compare(password, user.password);
    if (!passwordValid) {
      console.info(`Invalid password for ${usernameOrEmail}`);
      throw createUnauthorizedError('Invalid credential');
    }

    console.info(`Success auth for ${usernameOrEmail}`);

    setJwtCookies(fastify, reply, user, 'both');

    return reply.send({
      data: { success: true },
      message: 'Login successful',
    });
  });

  fastify.post('/logout', async (_request, reply) => {
    // const { access_token: accessToken, refresh_token: refreshToken } = request.cookies;
    // const authHeader = request.headers.authorization;
    // await fastify.redis.set(`blacklist:${token}`, 'true', 'EX', Number(process.env.JWT_EXPIRES_IN));

    clearJwtCookies(reply, 'both');

    reply.send({
      data: { success: true },
      message: 'Logout successful',
    });
  });
}
