import { UserInfoResponse } from '@/generated/user';
import { clearJwtCookies, createMeta4ServiceRequest, prepareString, setJwtCookies } from '@/utils';
import { createUnauthorizedError } from '@/utils/exceptions';
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
      fastify.userGrpc.findUserByEmailOrUsername({ usernameOrEmail }, metadata, (error, response) => {
        if (error) {
          reject(error);
        }
        resolve(response);
      });
    });

    const passwordValid = await bcrypt.compare(password, user.password);
    if (!passwordValid) {
      throw createUnauthorizedError('Invalid credential');
    }

    setJwtCookies(fastify, reply, user, 'both');

    return reply.send({
      data: { success: true },
      message: 'Login successful',
    });
  });

  fastify.post('/logout', async (request, reply) => {
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
