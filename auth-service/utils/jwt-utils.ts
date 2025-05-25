import { UserInfoResponse } from "@/generated/user";
import { CookieSerializeOptions } from "@fastify/cookie";
import { Metadata } from "@grpc/grpc-js";
import { FastifyInstance, FastifyReply } from "fastify";
import { PrivateKey, PublicKey, Secret, sign, SignOptions } from 'jsonwebtoken';
import { isLocalServer } from ".";
import { getTokenInfo, TokenInfoCompanion } from "./shared-lib-wrapper";

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
      isAccess ? TokenInfoCompanion.ACCESS_COOKIE_NAME : TokenInfoCompanion.REFRESH_COOKIE_NAME,
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

interface IWithId {
  id: string;
}
interface IUser extends IWithId {
  username: string;
  email: string;
  roles: string[];
};

type IUserInfo<Type extends boolean> = Type extends true ? IUser : IWithId;

export enum TokenType {
  ACCESS,
  REFRESH,
  UNDEFINED
}

export class JwtService {
  private _secretKey: Secret | PrivateKey;
  private _publicKey: PublicKey;
  public readonly refreshExpires: number;
  public readonly accessExpires: number;

  constructor(
    secretKey: Secret | PrivateKey,
    publicKey: PublicKey,
    accessExpires: number,
    refreshExpires: number
  ) {
    this._secretKey = secretKey;
    this._publicKey = publicKey;
    this.refreshExpires = refreshExpires;
    this.accessExpires = accessExpires;
  }

  sign = <Type extends boolean>(
    isAccessToken: Type,
    user: IUserInfo<Type> | 'service',
  ): string => {
    const payload = {
      type: isAccessToken ? 'access' : 'refresh',
    };
    if (isAccessToken) {
      const { username, roles, email } = user as IUser;
      Object.assign(payload, { username, roles, email });
    }
    const opts: SignOptions = { algorithm: 'RS256' };
    if (user === 'service') {
      Object.assign(payload, { username: user, email: 'ilogos@ilogos.ru' });
      opts.expiresIn = '1Minute';
      opts.subject = user;
    } else {
      opts.expiresIn = isAccessToken ? this.accessExpires : this.refreshExpires;
      opts.subject = user.id;
    }

    console.debug(`Sign jwt: ${opts.subject}`);

    return sign(payload, this._secretKey, opts);
  }

  getTokenInfo = (token: string) => getTokenInfo(token, this._publicKey);
}
