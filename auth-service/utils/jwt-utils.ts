import { UserInfoResponse } from "@/generated/user";
import { CookieSerializeOptions } from "@fastify/cookie";
import { Metadata } from "@grpc/grpc-js";
import { FastifyInstance, FastifyReply } from "fastify";
import { isLocalServer } from ".";

const getTokenCookieName = (isAccess: boolean) => isAccess ? 'access_token' : 'refresh_token';

const getTokenCookieDetails = (isAccess: boolean, secure: boolean, fastify: FastifyInstance | null): CookieSerializeOptions => {
  const path = isAccess ? '/' : '/api/auth/refresh';
  const maxAge = fastify ? (
    isAccess ?
      fastify.jwt.accessExpires
      : fastify.jwt.refreshExpires
  ) : 0;

  return ({
    path,
    httpOnly: true,     // недоступна из JS (безопасность)
    secure,             // Включить в production (https)
    sameSite: 'strict', // Защита от CSRF
    maxAge
  });
};

const _setJwtCookies = (
  reply: FastifyReply,
  opts: {
    user: UserInfoResponse;
    fastify: FastifyInstance;
  } | null,
  tokens: 'both' | 'access' | 'refresh'
) => {
  const secure = !isLocalServer();

  const tokenTypes = tokens === 'both' ? [true, false] : [tokens !== 'refresh'];
  for (const isAccess of tokenTypes) {
    const token = opts ? opts.fastify.jwt.sign(isAccess, opts.user) : 'logout';
    const cookieDetails = getTokenCookieDetails(isAccess, secure, opts ? opts.fastify : null);

    reply.setCookie(
      getTokenCookieName(isAccess),
      token,
      cookieDetails);
  }
}

export const setJwtCookies = (
  fastify: FastifyInstance,
  reply: FastifyReply,
  user: UserInfoResponse,
  tokens: 'both' | 'access' | 'refresh'
) => _setJwtCookies(reply, { user, fastify }, tokens);

export const clearJwtCookies = (
  reply: FastifyReply,
  tokens: 'both' | 'access' | 'refresh'
) => _setJwtCookies(reply, null, tokens);

export const createMeta4ServiceRequest = (fastify: FastifyInstance) => {
  const metadata = new Metadata();
  const requestJwt = fastify.jwt.sign(true, 'service');
  metadata.add('authorization', `Bearer ${requestJwt}`)
  return metadata;
}
