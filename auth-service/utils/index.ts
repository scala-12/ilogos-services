export const prepareString = (...strings: unknown[]): string | null => {
  for (const str of strings) {
    if (typeof str === 'string' && str.trim().length) {
      return str;
    }
  }

  return null;
}

export const isLocalServer = () => process.env.NODE_ENV === 'local';
