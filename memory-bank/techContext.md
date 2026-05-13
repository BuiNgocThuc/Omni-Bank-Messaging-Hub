# Tech Context

## Technologies Used
- **Language**: Java 21
- **Framework**: Spring Boot 4.0.6
- **Build Tool**: Maven (multi-module project)
- **Database**: Oracle Database (using `ojdbc11` version 23.3.0.23.09)
- **Utilities**: Lombok 1.18.36
- **Infrastructure**: Docker & Docker Compose for local deployment

## Development Setup
- Standard Maven multi-module structure.
- Local environment uses `docker-compose.yml` for infrastructure.

## Technical Constraints
- High consistency required for financial transactions.
- FX rates are only valid for 30 seconds.
- Adherence to strict trading hours (08:00 to 16:30, Mon-Fri).
