# Testing

## Automated tests (Maven)

Run tests for all modules:

```bash
mvn test
```

## Manual end-to-end demo (Docker)

Start the stack:

```bash
mvn -DskipTests clean package
docker compose up --build
```

Then follow the end-to-end demo in the repository `README.md` (Create users → menu → order → pay → delivery).
