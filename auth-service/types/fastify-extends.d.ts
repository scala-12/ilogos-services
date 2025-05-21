import { UserServiceClient } from '@/generated/user';
import { JwtService } from '@/utils/jwt-utils';

declare module 'fastify' {
  interface FastifyInstance {
    jwt: JwtService,
    userGrpc: UserServiceClient,
    // redis: RedisClientType,
  }
}
