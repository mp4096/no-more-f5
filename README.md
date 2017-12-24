# No more F5!

[![Build Status](https://travis-ci.org/mp4096/no-more-f5.svg?branch=master)](https://travis-ci.org/mp4096/no-more-f5)

I procrastinate a lot by reloading webpages, looking for new content.
However, I don't like being a Skinner box rat,
so I wrote this digest generator to tame my FOMO.

Since this is my Clojure learning project, this program is very brittle.
If it cannot scrape a feed (e.g. due to a 403), it crashes in an unpleasant way.
I'm going to work on error handling in future.

## Installation

### Cloud stack

Rough idea of the required cloud stack:

* Emails are sent using AWS SES via the SMTP protocol.
* The function itself is deployed to AWS Lambda and is triggered
  by a scheduled CloudWatch event (cron).

If you want to know, here's the motivation for this stack:

> __Why SMTP protocol?__
>
> Since we need to scrape RSS feeds, we need Internet access.
> This can be configured in two ways:
>
> * Place the Lambda function outside a VPC and connecting to SES via SMTP.
> * Place the Lambda function inside a VPC and route Internet traffic through a NAT Gateway.
>   In this case we can talk to SES directly.
>
> I use the former way.
> Although SMTP emails cost a little bit more,
> configuring a VPC and a NAT Gateway is tedious
> and a NAT Gateway is certainly much more expensive than the SMTP emails.
> However, if you already have one, you can certainly try it. YMMV.

### Building and packaging your function

You will need Leiningen to build your uberjar.
But first, create a list of your Atom/RSS feeds and save it in a file, e.g. `my_feeds`:

```sh
$ cat > my_feeds <<EOF
https://github.com/BurntSushi/ripgrep/releases.atom
https://github.com/atom/atom/releases.atom
EOF
```

Now we build a standalone uberjar and add `my_feeds` to it
(remember, jars are just zip archives).
This process is automated in `prepare_package.sh`
(specify your feeds file as a call parameter):

```sh
$ ./prepare_package.sh my_feeds
```

### Preparing your SES

1. Verify your email address in SES.
1. Create SMTP credentials and save them -- we'll need them later.

_Important:_ Creating SMTP credentials also creates an IAM user.
Do not use this user's credentials for the SMTP server!


### Creating and configuring the Lambda function

1. Create a new Lambda function.
1. Use a standard IAM role, just enough to store CloudWatch logs.
1. Select Java 8 as runtime.
1. Add a CloudWatch event as a trigger. Schedule it to something like `cron(0 6 * * ? *)`,
   i.e. every day at 6:00 UTC.
1. Choose something around 384 MB memory and 90 seconds timeout
   (depends heavily on the number of feeds you want to digest).
1. Set handler to `no_more_f5.core::handler`
1. Now we need to setup environment variables. Add following envvars:

| Variable      | Note                                      | Example                              |
|:--------------|:------------------------------------------|:-------------------------------------|
| `FEEDS`       | Filename of the file with your feed URLs  | `my_feeds`                           |
| `USER_AGENT`  | See below                                 | `Mozilla/5.0 ...`                    |
| `SMTP_SERVER` | Address of your AWS SES SMTP server       | `email-smtp.eu-west-1.amazonaws.com` |
| `SMTP_PORT`   | SMTP server port, check out your SES docs | `587`                                |
| `SMTP_USER`   | Use your SES SMTP credentials here        |                                      |
| `SMTP_PASS`   | Use your SES SMTP credentials here        |                                      |
| `EMAIL_FROM`  | Must be verified in AWS SES               | `jane.doe@abc.com`                   |
| `EMAIL_TO`    | All of them must be verified in AWS SES   | `jane.doe@abc.com, john.doe@abc.com` |

You need to specify `USER_AGENT` since some sites block scrapers without it.
Just use something similar to your main browser.

Ok, you should be ready to go! Create a dummy testing event
(just use an empty dict `{}` as context) and see if you've got a digest in your inbox!

### Configuring CloudWatch logs retention

One more thing:
Go to CloudWatch and configure log retention for your `no-more-f5` log group.
Set it to something reasonable, e.g. 7 days.
Storing a lot of logs (several GBs) might be expensive and it's just not worth it in this case.

### How much is the fish?

No idea, I'll update this when I get my first monthly bill. But probably not much.
