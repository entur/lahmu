#! /bin/bash

bucketURL="gs://entur-docs.appspot.com/bikeservice"

echo "Updating documentation at $bucketURL"
rsync -R **/*.mdx temp-docs && gsutil -m rsync -d -r temp-docs $bucketURL && rm -rf temp-docs
echo "Done!"
