# System Patterns

## Architecture
- **Microservices Architecture**: The system is broken down into specialized services (SF Service, SF Processor Service, Treasury Service, Core Banking Service).
- **Event-Driven Architecture**: Uses Message Queues (MQ) for asynchronous processing of FX requests.

## Key Technical Decisions
- **Idempotency**: `idempotency_key` is used to prevent duplicate requests.
- **Double Entry Accounting**: Core Banking service manages ledger entries (`HOLD`, `RELEASE`, `DEBIT`, `CREDIT`).
- **Saga/Workflow Pattern**: The FX transaction is processed in steps across multiple services, with SF Processor orchestrating the workflow.
- **Eventual Consistency**: Immediate 202 response to client, with processing happening in the background.

## Component Relationships
1. **SF Service**: Receives client requests, validates, checks idempotency, pushes to MQ.
2. **SF Processor Service**: Consumes MQ messages, fetches rates from Treasury, orchestrates the core banking updates.
3. **Treasury Service**: Provides real-time exchange rates (valid for 30s).
4. **Core Banking Service**: Manages accounts, holds balances, and creates ledger entries.
