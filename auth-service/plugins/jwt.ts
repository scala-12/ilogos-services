import fp from 'fastify-plugin';
import fs from 'fs/promises';
import jwt, { PrivateKey, PublicKey, Secret } from 'jsonwebtoken';
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
    user: IUserInfo<Type>,
  ): string => {
    const payload = {
      id: user.id,
      type: isAccessToken ? 'access' : 'refresh',
    };
    if (isAccessToken) {
      const { username, roles, email } = user as IUser;
      Object.assign(payload, { username, roles, email });
    }

    return jwt.sign(
      payload,
      this._secretKey,
      { expiresIn: isAccessToken ? this.accessExpires : this.refreshExpires });
  }

  // const verifyJwt = (token: string): JwtPayload | null => {
  //   try {
  //     return jwt.verify(token, JWT_SECRET) as JwtPayload;
  //   } catch (err) {
  //     return null;
  //   }
  // }
}

export default fp(async function (fastify) {
  console.debug("secret.pem read");
  const secretKeyContent = await fs.readFile(
    path.join(process.env.JWT_SECRET_KEY_PATH as string),
    'utf-8');
  const secretKey = secretKeyContent.replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "").replaceAll(/\s+/g, '');

  console.debug("public.pem read");
  const publicKeyContent = await fs.readFile(
    path.join(process.env.JWT_PUBLIC_KEY_PATH as string),
    'utf-8');
  const publicKey = secretKeyContent.replace("-----BEGIN PUBLIC KEY-----", "")
    .replace("-----END PUBLIC KEY-----", "").replaceAll(/\s+/g, '');

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
