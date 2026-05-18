/**
 * AudioRoot — mounts both MiniAudioPlayer and FullPlayer as a single island.
 * Include once per page: <AudioRoot client:load />
 */
import { MiniAudioPlayer } from "./MiniAudioPlayer";
import { FullPlayer } from "./FullPlayer";

export function AudioRoot() {
  return (
    <>
      <MiniAudioPlayer />
      <FullPlayer />
    </>
  );
}
