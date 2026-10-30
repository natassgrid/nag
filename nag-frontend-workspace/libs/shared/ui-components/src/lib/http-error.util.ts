export interface ParsedHttpError {
  message: string;
  fieldErrors: Record<string, string>;
}

/**
 * Normalizes backend HTTP error responses across all NAG microservices
 * (RFC 7807 ProblemDetail, ApiErrorResponse, ApiResponse, ErrorEnvelope)
 * into a human-readable summary message and a field-level error map.
 */
export function parseHttpError(err: unknown, fallbackMessage = 'An unexpected error occurred. Please check your inputs and try again.'): ParsedHttpError {
  const errorObj = (err as { error?: Record<string, unknown>; message?: string; status?: number }) || {};
  const payload = (errorObj.error && typeof errorObj.error === 'object' ? errorObj.error : {}) as Record<string, unknown>;

  const rawFieldErrors =
    (payload['fieldErrors'] as Record<string, string> | undefined) ||
    ((payload['properties'] as Record<string, unknown> | undefined)?.['fieldErrors'] as Record<string, string> | undefined) ||
    {};

  const fieldErrors: Record<string, string> = {};
  if (rawFieldErrors && typeof rawFieldErrors === 'object' && !Array.isArray(rawFieldErrors)) {
    for (const [key, val] of Object.entries(rawFieldErrors)) {
      if (typeof val === 'string' && val.trim()) {
        fieldErrors[key] = humanizeConstraintMessage(key, val.trim());
      }
    }
  }

  const errorsArray = Array.isArray(payload['errors']) ? (payload['errors'] as unknown[]) : [];
  if (errorsArray.length > 0 && Object.keys(fieldErrors).length === 0) {
    for (const item of errorsArray) {
      if (typeof item === 'string' && item.includes(':')) {
        const [field, ...rest] = item.split(':');
        if (field && rest.length > 0) {
          fieldErrors[field.trim()] = humanizeConstraintMessage(field.trim(), rest.join(':').trim());
        }
      }
    }
  }

  const fieldErrorValues = Object.values(fieldErrors);
  if (fieldErrorValues.length > 0) {
    return {
      message: fieldErrorValues.join(' • '),
      fieldErrors,
    };
  }

  const detail = typeof payload['detail'] === 'string' ? payload['detail'].trim() : '';
  const msg = typeof payload['message'] === 'string' ? payload['message'].trim() : '';
  const errProp = typeof payload['error'] === 'string' ? payload['error'].trim() : '';

  const isGenericSummary = (text: string): boolean => {
    const lower = text.toLowerCase();
    return (
      lower === 'one or more request fields failed validation.' ||
      lower === 'validation failed for one or more fields' ||
      lower === 'validation failed' ||
      lower === 'bad request'
    );
  };

  if (detail && !isGenericSummary(detail)) {
    return { message: detail, fieldErrors };
  }
  if (msg && !isGenericSummary(msg)) {
    return { message: msg, fieldErrors };
  }
  if (errorsArray.length > 0) {
    const joined = errorsArray.filter((x): x is string => typeof x === 'string').join(' • ');
    if (joined) return { message: joined, fieldErrors };
  }
  if (detail) {
    return { message: detail, fieldErrors };
  }
  if (msg) {
    return { message: msg, fieldErrors };
  }
  if (errProp && !isGenericSummary(errProp)) {
    return { message: errProp, fieldErrors };
  }

  return { message: fallbackMessage, fieldErrors };
}

export function extractHttpErrorMessage(err: unknown, fallbackMessage = 'An unexpected error occurred. Please check your inputs and try again.'): string {
  return parseHttpError(err, fallbackMessage).message;
}

function humanizeConstraintMessage(field: string, rawMessage: string): string {
  if (rawMessage.includes('^[6-9]') || (field.toLowerCase().includes('mobile') && rawMessage.includes('must match'))) {
    return 'Mobile number must be a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.';
  }
  if (rawMessage === 'must not be blank' || rawMessage === 'must not be null' || rawMessage === 'must not be empty') {
    const readableField = field.replace(/([A-Z])/g, ' $1').replace(/^./, (s) => s.toUpperCase());
    return `${readableField} is required.`;
  }
  return rawMessage;
}
