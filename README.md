## Micronaut 2.5.9 Documentation

- [User Guide](https://docs.micronaut.io/2.5.9/guide/index.html)
- [API Reference](https://docs.micronaut.io/2.5.9/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/2.5.9/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

## Feature lombok documentation

- [Micronaut Project Lombok documentation](https://docs.micronaut.io/latest/guide/index.html#lombok)

- [https://projectlombok.org/features/all](https://projectlombok.org/features/all)


## Preconditions

- Install Java 11

- Install Maven

## How to run server

- Build server from CLI: mvn clean install

- Run server java -jar /home/osboxes/grpc-java-chat/target/party-participant-service-0.1.jar

## How to run test client

- Build server from CLI: mvn clean install

- Run client java -cp /home/osboxes/grpc-java-chat/target/party-participant-service-0.1.jar com.party.participant.PartyClient

#General architecture
 - We have 3 predefined users: jack, max, andy. Use it to login (passwords now hardcoded)
 - health channel used to send health call from mobile app. If no health calls during 2 minutes, user is automatically logged out from
 - Enter LOGOUT to leave party.

#GRPC operations:
   Login - join party
   Logout - leave party
   health - channel for receive notification, that mobile app still leave.

#Restrictions:
   We don't support multiple parties now. All users are joining the same party.