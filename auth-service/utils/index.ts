import { UserInfoResponse } from "@/generated/user";
import { Metadata } from "@grpc/grpc-js";
import { FastifyInstance, FastifyReply } from "fastify";

export const prepareString = (...strings: unknown[]): string | null => {
  for (const str of strings) {
    if (typeof str === 'string' && str.trim().length) {
      return str;
    }
  }

  return null;
}

export const isLocalServer = () => process.env.NODE_ENV === 'local';

export const setJwtCookies = (
  fastify: FastifyInstance,
  reply: FastifyReply,
  user: UserInfoResponse,
  tokens: 'both' | 'access' | 'refresh'
) => {
  const secure = !isLocalServer();

  const tokenTypes = tokens === 'both' ? [true, false] : [tokens !== 'refresh'];
  for (const isAccess of tokenTypes) {
    const path = isAccess ? '/' : '/api/auth/refresh';
    const token = fastify.jwt.sign(isAccess, user);
    const maxAge = isAccess ?
      fastify.jwt.accessExpires
      : fastify.jwt.refreshExpires;

    reply.setCookie(
      isAccess ? 'access_token' : 'refresh_token',
      token,
      {
        path,
        httpOnly: true,     // недоступна из JS (безопасность)
        secure,             // Включить в production (https)
        sameSite: 'strict', // Защита от CSRF
        maxAge
      });
  }
}

export const createMeta4ServiceRequest = (fastify: FastifyInstance) => {
  const metadata = new Metadata();
  const requestJwt = fastify.jwt.sign(true, 'service');
  metadata.add('authorization', `Bearer ${requestJwt}`)
  return metadata;
}