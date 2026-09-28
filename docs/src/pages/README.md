# Building Real Applications in Scala Native

This is the content for the [Scala Native][scala-native] workshop delivered at Scala Days 2026. The overall goal of the workshop is to introduce developers to using Scala Native. In particular:

- project setup and sbt commands for using Scala Native;
- interfacing with the archaic world of C and system calls; and
- interacting with the terminal at a low and high level.

Our goal is to end with a simple terminal application built using Scala Native.


[scala-native]: https://scala-native.org/en/latest/


## What is Scala Native?

Before we go further, we should talk a bit about what Scala Native is.


## Why is Scala Native?


## Development

Write code and then run `build` in sbt. Watch it do everything (compile code, run tests, format code, check dependencies are up to date, build your documentation, and more!)

The `build.sbt` defines a single project, called `core`. All your code should be under the `core` project, unless you create additional projects. By default `core` is defined as a cross platform project. If you don't need a cross platform project you can reduce some complexity by changing it to a normal project.

If you create additional sub-projects, make sure to aggregate them in the `root` project. The `root` project should not contain any code. It only exists to aggregate other projects.


## Documentation

Write your documentation in `docs/src/pages`. You'll find this documentation there!

You can write Scala code and use mdoc to evaluate it, as shown below.

```scala mdoc
List(1,2,3).map(_ * 2)
```

You can include images in the usual way. Here is a picture of a chessboard!

![A picture of a red and black chessboard](chessboard.png)

If your documentation needs extended code examples, you can write that code in the `examples` project and pull it into the documentation. There is an example below, rendered by Scala.js code in the `examples` project.

@:doodle("hello", "Example.draw")

When working on the documentation, if is often useful to just build the documentation without doing all the other time consuming stuff that `build` does. The sbt command to do this is `docs / tlSite`. For a live preview run the command `docs / tlSitePreview`.

To publish your documentation you'll need to set the following settings on your Github project (taken from the [sbt-typelevel-site] documentation)

1. Grant "Read and write" permissions to workflows. This enables them to push to the gh-pages branch. `https://github.com/{user}/{repo}/settings/actions`
2. Set the GitHub pages source to the / (root) directory on the gh-pages branch. `https://github.com/{user}/{repo}/settings/pages`

Once set, your documentation will be published on each push to the `main` branch.


## Continuous Integration

Running `build` will generate Github Actions workflows to run essentially the same steps as `build`, as well as publishing your documentation. Simply commit the files in the `.github` directory and it will work.


## Publishing

Push a tag like `v0.2.0`, replacing the number with an appropriate version number for your project, and it will be published to Maven. You will need to have added appropriate secrets to your repository. See the [sbt-typelevel documentation](https://typelevel.org/sbt-typelevel/secrets.html) for the details.

When you create breaking changes you will need to bump the version in `tlBaseVersion` to get CI happy again.

[mdoc]: https://scalameta.org/mdoc/
[laika]: https://typelevel.org/Laika/
[creative-scala-theme]: https://github.com/creativescala/creative-scala-theme/
[sbt-typelevel]: https://typelevel.org/sbt-typelevel/
[sbt-typelevel-site]: https://typelevel.org/sbt-typelevel/site.html
