import { UserInfoResponse } from '@/generated/user';
import { prepareString } from '@/utils';
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

    const user = await new Promise<UserInfoResponse>((resolve, reject) => {
      fastify.userGrpc.findUserByEmailOrUsername({ usernameOrEmail }, (error, response) => {
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

    const accessToken = fastify.jwt.sign(true, user);
    const refreshToken = fastify.jwt.sign(false, user);
    const isLocal = process.env.NODE_ENV === 'local';

    reply.setCookie('access_token', accessToken, {
      path: '/',
      httpOnly: true,       // недоступна из JS (безопасность)
      secure: isLocal,      // Включить в production (https)
      sameSite: 'strict',   // Защита от CSRF
      maxAge: fastify.jwt.accessExpires
    });
    reply.setCookie('refresh_token', refreshToken, {
      path: '/api/auth/refresh',
      httpOnly: true,       // недоступна из JS (безопасность)
      secure: isLocal,      // только по HTTPS
      sameSite: 'strict',   // Защита от CSRF
      maxAge: fastify.jwt.refreshExpires
    });

    return reply.send({
      data: { success: true },
      message: 'Login successful',
    });
  });

  fastify.post('/logout', async (request, reply) => {
    const authHeader = request.headers.authorization;
    if (!authHeader) {
      return reply.code(401).send({ error: 'No token' });
    }
    const token = authHeader.replace('Bearer ', '');

    // Добавляем accessToken в blacklist в Redis с временем жизни токена
    // await fastify.redis.set(`blacklist:${token}`, 'true', 'EX', Number(process.env.JWT_EXPIRES_IN));

    reply.send({ status: 'logged out' });
  });
}
