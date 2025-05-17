import PostgresPlugin from '@fastify/postgres';
import fp from 'fastify-plugin';

export default fp(async function (fastify) {
  fastify.register(PostgresPlugin, {
    connectionString: process.env.DATABASE_URL,
  });
});
