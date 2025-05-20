import { UserServiceClient } from '@/generated/user';
import { JwtService } from '@/plugins/jwt';

declare module 'fastify' {
  interface FastifyInstance {
    jwt: JwtService,
    userGrpc: UserServiceClient,
    // redis: RedisClientType,
  }
}
