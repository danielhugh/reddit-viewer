# Reddit Viewer

A ClojureScript SPA built using the Re-frame framework to display Reddit posts by subreddit in a tabular view.

- [Reddit Viewer](#reddit-viewer)
  - [Live Demo](#live-demo)
  - [Installation and Setup Instructions](#installation-and-setup-instructions)
    - [Requirements](#requirements)
    - [Development mode](#development-mode)
    - [Building for production](#building-for-production)
  - [Troubleshooting](#troubleshooting)

## Live Demo

[View the live demo](https://reddit-viewer.danielhugh.vercel.app/)

## Installation and Setup Instructions

### Requirements

* Clojure
* npm

### Development mode

To start developing, navigate to the project folder and run the following command in the terminal:

```bash
# Install dependencies
npm install

# Starts an nREPL server on port 7002, and the shadow-cljs watcher
npm run dev
```

Browse to http://localhost:3449 to view the running app.

(Optional) To watch for CSS changes, run the following from another terminal:

```bash
npm run watch:css
```

### Building for production

Create the release build:

```bash
npm run build:css && npm run release
```

## Troubleshooting

The app will not work if Firefox's Enhanced Tracking Protection is enabled as it will block requests made to the Reddit JSON API.
