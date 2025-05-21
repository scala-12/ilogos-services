import { JwtService } from '@/utils/jwt-utils';
import fp from 'fastify-plugin';
import fs from 'fs/promises';
import path from 'path';

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
