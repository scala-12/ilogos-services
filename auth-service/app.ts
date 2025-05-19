import autoLoad from '@fastify/autoload';
import dotenv from 'dotenv';
import fastify from 'fastify';
import path from 'path';


console.debug(".env init");
// common config
dotenv.config({ path: path.resolve(process.cwd(), '.env') });
// special config
dotenv.config({ path: path.resolve(process.cwd(), `.env.${process.env.NODE_ENV}`) });
// config with temp overrides
dotenv.config({ path: path.resolve(process.cwd(), `.env.tmp`) });

const server = fastify()

server.register(autoLoad, {
  dir: path.join(__dirname, 'plugins')
});

server.register(autoLoad, {
  dir: path.join(__dirname, 'routes'),
  options: { prefix: '/api/auth' }
});

server.setErrorHandler((error, _, reply) => {
  const statusCode = error.statusCode ?? 500

  reply.status(statusCode).send({
    statusCode,
    error: error.code ?? 'InternalServerError',
    message: error.message ?? 'Something went wrong'
  });
});

const port = parseInt(process.env.PORT as string);
server.listen({ port }, (err, address) => {
  if (err) {
    console.error(err)
    process.exit(1)
  }
  console.log(`Server listening at ${address}`)
})
