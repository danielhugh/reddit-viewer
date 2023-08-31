
### Development mode

To start developing navigate to the project folder and run the following command in the terminal:

```
npx shadow-cljs watch app
```

Browser to http://localhost:8080 to view the running app.

### REPL

The project is setup to start nREPL on port `7002`.

### Building for production

Create the release build:

```
npx shadow-cljs release app
```
