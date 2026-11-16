"""Local fake player for the real D-Bus adapter integration test (no music service)."""
import asyncio
import sys
from pathlib import Path
from dbus_next import Variant
from dbus_next.aio import MessageBus
from dbus_next.service import ServiceInterface, dbus_property
from dbus_next.constants import PropertyAccess

class Player(ServiceInterface):
    def __init__(self):
        super().__init__('org.mpris.MediaPlayer2.Player')

    @dbus_property(access=PropertyAccess.READ)
    def PlaybackStatus(self) -> 's':
        return 'Playing'

    @dbus_property(access=PropertyAccess.READ)
    def Position(self) -> 'x':
        return 12500000

    @dbus_property(access=PropertyAccess.READ)
    def Metadata(self) -> 'a{sv}':
        return {'xesam:title': Variant('s', 'Integration Song'),
                'xesam:artist': Variant('as', ['Integration Artist'])}

async def main():
    bus = await MessageBus().connect()
    bus.export('/org/mpris/MediaPlayer2', Player())
    await bus.request_name('org.mpris.MediaPlayer2.VisionTest')
    Path(sys.argv[1]).touch()
    await asyncio.Future()

asyncio.run(main())
