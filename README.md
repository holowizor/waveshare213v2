### About
E-ink display driver for waveshare 2.13 v2 rewritten from python to Kotlin.

### Basic usage
Simply call
```
    Waveshare213.fullUpdate()
```
or
```
    Waveshare213.partialUpdate()
```
the create an image with white background and draw something with black color
```
    val img = BufferedImage(
        250, 122,
        BufferedImage.TYPE_BYTE_BINARY
    )

    val g2d = img.createGraphics()
    g2d.background = Color.white
    g2d.clearRect(0, 0, 250, 122)
    g2d.color = Color.BLACK
    g2d.drawLine(0, 0, 249, 121)
    g2d.drawString("Hello!", 20, 20)
    g2d.dispose()
```

and print it
```
    Waveshare213.printImage(img)
```

After you are done - put the display to sleep
```
    Waveshare213.sleep()
```

### Full update vs partial update
As per producer's documentation:
> Full refresh: e-Paper flicker when full refreshing to remove ghost image for best display.
> Partial refresh: It don't flicker if you use partial refresh (only some of the two-color e-paper support partial refresh). Note that you cannot use Partial refresh all the time, you should full refresh e-paper regularly, otherwise, ghost problem will get worse even damage.

### Original source
Full documentation with technical specification and original sources area available or the [producer's page](https://www.waveshare.com/wiki/2.13inch_e-Paper_HAT)