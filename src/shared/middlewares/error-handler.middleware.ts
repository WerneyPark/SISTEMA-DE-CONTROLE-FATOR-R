import { ApiResponse } from '../types/common.types';

export class AppError extends Error {
  public readonly statusCode: number;
  public readonly isOperational: boolean;

  constructor(message: string, statusCode = 400, isOperational = true) {
    super(message);
    this.statusCode = statusCode;
    this.isOperational = isOperational;
    Object.setPrototypeOf(this, new.target.prototype);
    Error.captureStackTrace(this);
  }
}

export function formatErrorResponse(error: unknown): ApiResponse<null> {
  const isAppErr = error instanceof AppError;
  const message = error instanceof Error ? error.message : 'Erro interno do servidor';
  
  return {
    success: false,
    error: message,
    timestamp: new Date().toISOString()
  };
}
