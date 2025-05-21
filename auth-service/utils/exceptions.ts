import createError from '@fastify/error';

export const createBadRequiestError = (msg: string) => new (createError('Bad Request', msg, 400));
export const createUnauthorizedError = (msg: string) => new (createError('Unauthorized', msg, 401));
export const createForbiddenError = (msg: string) => new (createError('Forbidden', msg, 403));
