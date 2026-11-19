"""Local fake player for the real D-Bus adapter integration test (no music service)."""
import asyncio
import sys
from pathlib import Path
from dbus_next import Message, MessageType, Variant
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
                'xesam:albumArtist': Variant('as', ['Integration Artist']),
                'xesam:comment': Variant('as', ['x' * 19000])}

def omit_position_from_get_all(message):
    # Some players only implement Position through Properties.Get.
    if (message.message_type == MessageType.METHOD_CALL
            and message.path == '/org/mpris/MediaPlayer2'
            and message.interface == 'org.freedesktop.DBus.Properties'
            and message.member == 'GetAll'
            and message.body == ['org.mpris.MediaPlayer2.Player']):
        metadata = {'xesam:title': Variant('s', 'Integration Song'),
                    'xesam:albumArtist': Variant('as', ['Integration Artist']),
                    'xesam:comment': Variant('as', ['x' * 19000])}
        return Message.new_method_return(message, 'a{sv}', [{
            'PlaybackStatus': Variant('s', 'Playing'),
            'Metadata': Variant('a{sv}', metadata),
        }])

async def main():
    bus = await MessageBus().connect()
    bus.add_message_handler(omit_position_from_get_all)
    bus.export('/org/mpris/MediaPlayer2', Player())
    await bus.request_name('org.mpris.MediaPlayer2.VisionTest')
    Path(sys.argv[1]).touch()
    await asyncio.Future()

asyncio.run(main())
