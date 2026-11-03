// Speech input (Web Speech API, Chrome/Edge) and text-to-speech (speechSynthesis), both free and built in.
// Chrome sends recognised audio to Google's servers; Firefox has no recognition, so the mic is hidden there.
import { useCallback, useEffect, useRef, useState } from 'react';

/** Course language → BCP-47 tag, e.g. "Spanish" → "es-ES" (same map as Spoken.locale on Android). */
export function speechLang(language) {
  const l = (language || '').trim().toLowerCase();
  if (/^(span|españ|espan|castell)/.test(l)) return 'es-ES';
  if (l.startsWith('catal')) return 'ca-ES';
  if (l.startsWith('engl')) return 'en-GB';
  if (/^(fren|franç)/.test(l)) return 'fr-FR';
  if (l.startsWith('ital')) return 'it-IT';
  if (/^(germ|deutsch)/.test(l)) return 'de-DE';
  if (l.startsWith('portug')) return 'pt-PT';
  return 'es-ES';
}

const Recognition = typeof window !== 'undefined' && (window.SpeechRecognition || window.webkitSpeechRecognition);

/** Picks a voice for `lang`, preferring an exact match ("es-ES") over any voice of the language ("es"). */
function voiceFor(lang) {
  const voices = window.speechSynthesis?.getVoices() ?? [];
  const base = lang.split('-')[0];
  return voices.find((v) => v.lang === lang) ?? voices.find((v) => v.lang?.startsWith(base)) ?? null;
}

/**
 * { canListen, listening, listen(onResult), stop(), canSpeak, speak(text) } for one language.
 * `onResult` gets up to five guesses, best first.
 */
export function useSpeech(lang) {
  const [listening, setListening] = useState(false);
  const [canSpeak, setCanSpeak] = useState(false);
  const recognition = useRef(null);

  useEffect(() => {
    if (!window.speechSynthesis) return undefined;
    const update = () => setCanSpeak(!!voiceFor(lang));
    update();
    window.speechSynthesis.addEventListener?.('voiceschanged', update);
    return () => window.speechSynthesis.removeEventListener?.('voiceschanged', update);
  }, [lang]);

  useEffect(() => () => recognition.current?.abort(), []);

  const listen = useCallback(
    (onResult) => {
      if (!Recognition) return;
      recognition.current?.abort();
      const r = new Recognition();
      r.lang = lang;
      r.maxAlternatives = 5;
      r.interimResults = false;
      r.continuous = false;
      r.onresult = (e) => {
        const result = e.results[0];
        const guesses = Array.from({ length: result.length }, (_, i) => result[i].transcript);
        if (guesses.length) onResult(guesses);
      };
      r.onend = () => setListening(false);
      r.onerror = (e) => {
        console.warn('Speech recognition error', e.error);
        setListening(false);
      };
      recognition.current = r;
      setListening(true);
      r.start();
    },
    [lang],
  );

  const stop = useCallback(() => recognition.current?.stop(), []);

  const speak = useCallback(
    (text) => {
      if (!window.speechSynthesis || !text) return;
      window.speechSynthesis.cancel();
      const u = new SpeechSynthesisUtterance(text);
      u.lang = lang;
      const voice = voiceFor(lang);
      if (voice) u.voice = voice;
      window.speechSynthesis.speak(u);
    },
    [lang],
  );

  return { canListen: !!Recognition, listening, listen, stop, canSpeak, speak };
}

/** "🎤 Say the sentence" / "Listening… (tap to stop)". Hidden where recognition isn't supported. */
export function MicButton({ speech, onResult, label = 'Say the sentence' }) {
  if (!speech.canListen) return null;
  return (
    <button type="button" className="mic" onClick={() => (speech.listening ? speech.stop() : speech.listen(onResult))}>
      {speech.listening ? '● Listening… (tap to stop)' : `🎤 ${label}`}
    </button>
  );
}

/** "🔊 Listen". Hidden when there is no voice for the language. */
export function ListenButton({ speech, text }) {
  if (!speech.canSpeak) return null;
  return (
    <button type="button" className="listen" onClick={() => speech.speak(text)}>
      🔊 Listen
    </button>
  );
}
