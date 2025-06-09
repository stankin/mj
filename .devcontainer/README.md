# Development Container for Modules Journal

This directory contains configuration for a development container that provides a consistent development environment for the Modules Journal project.

## Features

- Java 8 development environment
- Maven 3.8.6 for build management
- Kotlin 1.8.10 support
- PostgreSQL database for development
- PostgreSQL client tools
- VS Code extensions for Java, Kotlin, Maven, XML, and Docker

## Usage

### Prerequisites

- [Docker](https://www.docker.com/products/docker-desktop) installed on your machine
- [Visual Studio Code](https://code.visualstudio.com/) with the [Remote - Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers) extension installed

### Opening the Project in the Dev Container

1. Open the project folder in VS Code
2. When prompted to "Reopen in Container", click "Reopen in Container"
   - Alternatively, press F1, type "Remote-Containers: Reopen in Container" and press Enter

### Manual Start

If you're not using VS Code or the automatic prompt doesn't appear:

1. Start the containers with:
   ```
   docker-compose -f docker-compose-dev.yaml -f .devcontainer/docker-compose.yml up -d
   ```

   This command will start both the PostgreSQL database and the development container.

   Note: The docker-compose.yml file is configured to work correctly when run from the project root directory.

2. Connect to the running container using your preferred method

### Database Access

The PostgreSQL database is accessible at:
- Host: localhost (or pg-dev from within the container)
- Port: 5432
- Username: postgres
- Password: mj

### Building and Running the Application

Once inside the container, you can build and run the application using Maven:

```bash
# Build the application
mvn clean install

# Run the application (if applicable)
mvn wildfly:run
```

## Customization

You can customize the development environment by:

1. Modifying the Dockerfile to add additional tools or dependencies
2. Adding extensions to the devcontainer.json file
3. Adjusting environment variables in docker-compose.yml
