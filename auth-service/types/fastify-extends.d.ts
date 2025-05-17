import { JwtService } from '@/plugins/jwt';
import 'fastify';

declare module 'fastify' {
  interface FastifyInstance {
    jwt: JwtService,
    // redis: RedisClientType,
  }
}
