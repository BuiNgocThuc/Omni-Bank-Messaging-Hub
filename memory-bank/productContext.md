# Product Context

## Purpose
The system provides a platform for processing Foreign Exchange (FX) transactions securely and reliably. 

## Problems Solved
- Handling concurrent FX requests with proper balance locking (Holding).
- Ensuring eventual consistency and transactional integrity across microservices (Core Banking, Treasury, FX processing).
- Preventing duplicate transactions using idempotency keys.

## User Experience Goals
- Fast response (202 Accepted) for exchange requests by pushing to MQ.
- Transparent transaction statuses (PROCESSING, SUCCESS, FAILED).
- Enforced daily limits and trading hours for compliance.
