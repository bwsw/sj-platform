# Introduction

Stream Juggler project front-end

# CoreUI Theme Docs

http://coreui.io/docs/

# How to start

In order to start the project use:

```bash
git clone 
cd core/sj-frontend
# install the project's dependencies
npm install
# serve in develop mode
npm start
```

## Use NPM scripts
to build an optimized version of your application in /dist
```bash
npm run build.prod
```
to build and serve an optimized version of your application
```bash
npm run serve.prod
```


## Code style agreements
Use this article to https://github.com/Microsoft/TypeScript/wiki/Coding-guidelines 
to provide code that is easier to maintain, debug, and scale

## Commit policy
1. Create new feature branch from develop branch as SJ-<number of task>
2. Name your commit as SJ-<number of task> <description>
3. Merge develop into your branch and resolve conflicts
4. Run tests
5. Merge your branch into develop
6. Delete branch
