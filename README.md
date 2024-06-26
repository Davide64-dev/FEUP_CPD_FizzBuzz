# CPD - Second Assign

## Run Server

```shell
javac -d bin -cp bin:lib/ $(find src -name "*.java") 
```

```shell
java -cp bin game.GameServer <PORT>
```

## Run Client

```shell
javac -d bin -cp bin:lib/ $(find src -name "*.java") 
```

```shell
java -cp bin game.GameClient <IP> <PORT>
```