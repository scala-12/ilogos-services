import rateLimit from '@fastify/rate-limit';
import fp from 'fastify-plugin';

export default fp(async function (fastify) {
  fastify.register(rateLimit, {
    max: 60,
    timeWindow: '1 minute',
    // redis: new Redis({ host: '127.0.0.1' }),
  });
});
