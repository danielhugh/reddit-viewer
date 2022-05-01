
### Development mode

To start developing navigate to the project folder and run the following command in the terminal:

```
clj -M:dev:shadow-cljs watch app
```

Browser to http://localhost:8080 to view the running app.

### REPL

The project is setup to start nREPL on port `7002`.

### Building for production

Create the release build:

```
clj -M:shadow-cljs release app
```
