# Reddit Viewer

A ClojureScript SPA built using the Re-frame framework to display Reddit posts by subreddit in a tabular view.

- [Reddit Viewer](#reddit-viewer)
  - [Live Demo](#live-demo)
  - [Installation and Setup Instructions](#installation-and-setup-instructions)
    - [Requirements](#requirements)
    - [Development mode](#development-mode)
    - [Building for production](#building-for-production)
  - [Troubleshooting](#troubleshooting)
  - [Reflection](#reflection)

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

## Reflection

The following were some goals for the project:

***Practice using and thinking in the re-frame framework, and using its more advanced features.***

This was largely achieved. I practiced working with the re-frame application state (`app-db`) and organized it following the [Indexed Entities Pattern](https://ericnormand.me/guide/database-structure-in-re-frame#Indexed-Entities-Pattern). I challenged myself to not use pre-made components (like those provided by the [re-com](https://github.com/day8/re-com) library) which forced me to think about the balance between local state and global state. The re-frame docs suggest that everything should be in the global `app-db`, but for localized forms that did not feel right to me. I ended up using local Reagent atoms to store transient state, and only interact with global state when capturing an intent that would change the rest of the app, like when submitting a form.

Some advances feature I found an opportunity to use was interceptors to validate the `app-db` and co-effects.

Future challenges would be to explore how to better break apart the app into smaller namespaces as the current organization would not scale in larger applications.

***Explore Clojure tooling and libraries.***

The application originally used [Figwheel](https://figwheel.org/) and [Leinengen](https://leiningen.org/). As the contemporary options are [Shadow-cljs](https://github.com/thheller/shadow-cljs) and [deps](https://clojure.org/guides/deps_and_cli), I navigated that conversion process. Since I had previous experience using Clojure spec, I wanted to dive deeper into [malli](https://github.com/metosin/malli) for validating the `app-db`. An advantage malli has is that it is pure data as opposed to macros so defining entities felt like destructuring. One thing that malli lacked out of the box which Clojure spec provided was specifying a collection of homogenous elements. As part of [dealing with order when using the Indexed Entities Pattern](https://ericnormand.me/guide/database-structure-in-re-frame#dealing-with-order) I stored the order of entities as a vector of keywords. It was not an issue as I was able to define a custom function but the documentation is not clear to me on what the best practice would be in this case. Ideally, I would have added it as a custom attribute to the vector type (somehow...), but I felt I would be going too much in the weeds for this basic use case.

Future challenges would be to learn how to break apart malli schemas (via a registry?) and import them because everything in the `app-db` is currently defined in one large map data structure.

***Explore interop with the Javascript and React ecosystem.***

Adding [Chart.js](https://www.chartjs.org/) came in the form of a Form-3 Reagent component which gave exposure to managing the React component lifecycle.

The [react-toastify](https://github.com/fkhadra/react-toastify) React library was used for notifications. In this case, React component interop was done using `reagent/adapt-react-class` (aka `:>` ). An interesting distraction was how to import the library's custom CSS as shadow-cljs doesn't provide a native way to do it. This lead to a rabbit hole of the [double bundle technique](https://code.thheller.com/blog/shadow-cljs/2020/05/08/how-about-webpack-now.html) and using Webpack as the external build tool. However, it didn't seem to be appropriate for my use case because all I wanted was bundled CSS but Webpack was outputting both a JavaScript and CSS bundle. This was on top of increasing the build time which decreased the interactivity provided by REPL driven development that I didn't like. This lead me to explore a CSS focused solution and I came across PostCSS, which seemed perfect. I was able to keep shadow-cljs as the tool for compiling JavaScript, and defer CSS to PostCSS. However, I am unsure how scalable this solution is because currently the CSS is global which can lead to conflicts in CSS naming (which CSS modules in JavaScript fix) for larger projects. Perhaps this is something that can be configured as part of the PostCSS processing pipeline.

Future challenges would be to explore utility CSS frameworks like [TailwindCSS](https://tailwindcss.com/) which appear to avoid the manual setup I did with PostCSS.

***Deploy the app to a publicly available URL.***

During my exploration with NextJS and the modern React ecosystem, I was introduced to Vercel as a deployment platform. I was impressed with how straightforward it made deploying the static tutorial application. However, since many default options were used, most of that process was automagical. I wanted to explore if I could deploy non-NextJS apps, and this was a perfect opportunity. As a ClojureScript app compiles to Javascript, it should be possible to deploy in a similar way. Since this app uses `npm` specifying the custom install, and build commands was all that was needed.

Future challenges would be to explore deploying a backend.

***Explore other technologies as desired.***

I have never used Google Analytics before prior to this project, so I tried it out here.
