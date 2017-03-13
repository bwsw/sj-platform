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

## Environment setting
Set API_BACKEND_HOST and API_BACKEND_PORT as Rest API host and port

## Commit policy for contributors

### Guidelines
1. Your commit must always compile
2. Your commit must work by itself
3. Use code style agreements
4. Do not forget to include all your files
5. Check twice before committing
6. Test your changes before committing
7. Announce changes in advance
8. Code review by other developers
9. Take responsibility for your commits
10. Don't commit code you don't understand
11. Don't commit if other developers disagree
12. Don't mix formatting changes with code changes