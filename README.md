# scrubdemomanual

See https://github.com/sauravrp/scrubdemo but without using Universal Image Loader.
 Using AsyncTask for loading images, cancelling pending image loads if the user already requested for new image to be loaded via scrubbing. Added a Memory LruCache.
 Also reusing bitmap on subsequent loading of the images since the image sizes are the same.
