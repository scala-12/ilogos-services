import { JwtService } from '@/plugins/jwt';

declare module 'fastify' {
  interface FastifyInstance {
    jwt: JwtService,
    // redis: RedisClientType,
  }
}
