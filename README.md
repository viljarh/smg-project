# Smart Greenhouse Project

This project is a distributed smart greenhouse application developed for the course [IDATA2304 Computer Communication and Network Programming (2023)](https://www.ntnu.edu/studies/courses/IDATA2304/2023) at NTNU. The application consists of sensor-actuator nodes and control panel nodes that communicate over a TCP/IP network.


## Getting Started

### Prerequisites

- Java 17
- Maven
- JavaFX

### Running the Application

There are several runnable classes in the project:

#### Greenhouse Simulation

To run the greenhouse part (with sensor/actuator nodes):

- **Command line version**: Run the `main` method inside the [`CommandLineGreenhouse`](src/main/java/no/ntnu/run/CommandLineGreenhouse.java) class.
- **GUI version**: Run the `main` method inside the [`GreenhouseGuiStarter`](src/main/java/no/ntnu/run/GreenhouseGuiStarter.java) class. Note: If you run the `GreenhouseApplication` class directly, JavaFX will complain that it can't find necessary modules.

#### Control Panel

To run the control panel (only GUI-version is available):

- Run the `main` method inside the [`ControlPanelStarter`](src/main/java/no/ntnu/run/ControlPanelStarter.java) class.

### Simulating Events

If you want to simulate fake communication (just some periodic events happening), you can run both the greenhouse and control panel parts with a command line parameter `fake`. Check out classes in the [`no.ntnu.run`](src/main/java/no/ntnu/run) package for more details.

## Communication Protocol

The communication protocol used in this project is described in detail in the [protocol.md](protocol.md) file. It includes information about the message formats, flow of information, and the underlying transport protocol.

## Security Note

The `.env` file containing sensitive information such as keystore paths and passwords is included in the public repository. This is generally considered bad practice as it exposes sensitive data. However, for the purposes of this school project and grading, it has been included.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Authors

- Viljar Hoem-Olsen
- Gaute Øye
- Eirik Imrik
- Thomas Åkre
