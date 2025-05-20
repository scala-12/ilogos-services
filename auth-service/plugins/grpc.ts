import { UserServiceClient } from '@/generated/user';
import { credentials } from '@grpc/grpc-js';
import fp from 'fastify-plugin';

export default fp(async function (fastify) {
  const client = new UserServiceClient(
    process.env.USER_SERVICE_GRPC_URL as string,
    credentials.createInsecure()
  );

  fastify.decorate('userGrpc', client);
});
