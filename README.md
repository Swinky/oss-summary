# OSS Summary Generator

A Java application that fetches data from GitHub repositories and generates rich, structured HTML summary reports using Azure OpenAI. The reports include overall activity, bug fixes, new features, improvements, and top contributors, with clickable links to original GitHub items.

## Features

- Fetches commits, pull requests, issues, and contributors from specified GitHub repositories.
- Uses Azure OpenAI to generate detailed HTML reports.
- Customizable prompt templates per repository.
- Configurable output directory for generated reports.
- Each report includes:
  - Overall summary (with generated summary paragraph)
  - Important bug fixes (with 1-2 line summaries)
  - New features (with 1-2 line summaries)
  - Improvements (with 1-2 line summaries)
  - Top contributors (with profile links)
  - Clickable links to all referenced PRs, commits, and issues
- High-level progress and timing logs for each step.

## Prerequisites

- Java 17+
- Maven
- Azure OpenAI API key and deployment
- GitHub API token (optional, can be set in config or via environment variable)

## Build Instructions

```sh
mvn clean install
```

## Run Instructions

After building, run the application using the generated JAR:

```sh
java -jar target/oss-summary-1.0-SNAPSHOT.jar
```

## Configuration

Edit `src/main/resources/config.properties` to set:

- `summary.period` - Number of days for the summary period
- `summary.endDate` - End date for the summary (YYYY-MM-DD)
- `summary.repositories` - Comma-separated list of repositories (e.g., `apache/incubator-gluten,facebook/velox`)
- `output.dir` - Output directory for HTML reports
- `github.token` - GitHub API token (optional, can be set via environment variable)
- `azure.agent.endpoint`, `azure.agent.apiKey`, `azure.agent.id` - Azure OpenAI configuration

## Prompt Templates

Customizable per repository. Place prompt files in `src/main/resources/prompts/` (e.g., `incubator-gluten.txt`, `velox.txt`, or `apache/incubator-gluten.txt`).
Prompts instruct the agent to generate HTML reports with the required structure and summaries.

## Output

- HTML reports are generated in the configured output directory (default: `output/`).
- Each file is named after the repository (e.g., `apache-incubator-gluten.html`).
- Reports include all required sections and clickable links.

## Logging

- Console logs show progress and time taken for each major step (fetching data, generating summaries, writing files).

## Testing

Run tests with Maven:

```sh
mvn test
```

Unit tests cover orchestrator logic, HTML output, error handling, and configuration.

## Example Usage

1. Configure `config.properties` and prompt templates.
2. Build and run the application.
3. Find HTML reports in the output directory.

## Contributing

- Fork the repository and create a feature branch.
- Submit pull requests with clear descriptions.
- Ensure all tests pass before submitting.

## License

MIT License (or specify your license here).

