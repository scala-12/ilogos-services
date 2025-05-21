import fp from 'fastify-plugin';
import fs from 'fs/promises';
import jwt, { PrivateKey, PublicKey, Secret, SignOptions, TokenExpiredError } from 'jsonwebtoken';
import path from 'path';

interface IId {
  id: string;
}
interface IUser extends IId {
  username: string;
  email: string;
  roles: string[];
};

type IUserInfo<Type extends boolean> = Type extends true ? IUser : IId;

export enum TokenType {
  ACCESS,
  REFRESH,
  UNDEFINED
}

interface AccessTokenInfo<HasPayload extends boolean> {
  type: TokenType.ACCESS;
  expired: boolean;
  id: HasPayload extends true ? string : undefined | string;
  email: HasPayload extends true ? string : undefined | string;
  username: HasPayload extends true ? string : undefined | string;
  roles: HasPayload extends true ? string[] : undefined | string[];
  hasPayload: HasPayload;
}

interface RefreshTokenInfo<HasPayload extends boolean> {
  type: TokenType.REFRESH;
  id: HasPayload extends true ? string : undefined | string;
  expired: boolean;
  hasPayload: HasPayload;
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

    return jwt.sign(payload, this._secretKey, opts);
  }

  getTokenInfo = (token: string): (
    { type: TokenType.UNDEFINED; expired?: boolean }
    | RefreshTokenInfo<false>
    | RefreshTokenInfo<true>
    | AccessTokenInfo<false>
    | AccessTokenInfo<true>
  ) => {
    let info;
    let isExpired = false;
    try {
      info = jwt.verify(token, this._publicKey);
    } catch (error) {
      if (!(error instanceof TokenExpiredError)) {
        throw error;
      }
      info = jwt.decode(token);
      isExpired = true;
    }

    if (info && typeof info !== 'string') {
      const expired = isExpired || info.exp == null || new Date(info.exp * 1_000) <= new Date();
      const id = info.sub || '';
      switch (info.type) {
        case 'access':
          const { username, email, roles } = info;
          return {
            expired, id, username, email, roles,
            type: TokenType.ACCESS,
            hasPayload: Boolean(id && username && email && roles)
          };
        case 'refresh':
          return {
            expired, id,
            type: TokenType.REFRESH,
            hasPayload: Boolean(id)
          };
        default:
          return { type: TokenType.UNDEFINED, expired };
      }
    }

    return { type: TokenType.UNDEFINED };
  }
}

export default fp(async function (fastify) {
  console.debug("secret.pem read");
  const secretKey = await fs.readFile(
    path.join(process.env.JWT_SECRET_KEY_PATH as string),
    'utf-8');

  console.debug("public.pem read");
  const publicKey = await fs.readFile(
    path.join(process.env.JWT_PUBLIC_KEY_PATH as string),
    'utf-8');

  const jwtService = new JwtService(
    secretKey,
    publicKey,
    parseInt(process.env.JWT_EXPIRES_IN as string),
    parseInt(process.env.JWT_REFRESH_EXPIRES_IN as string));

  fastify.decorate('jwt', jwtService);
  console.debug('Jwt service decorated');
}, {
  name: 'jwt-plugin'
});
