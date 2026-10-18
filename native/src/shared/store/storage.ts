import AsyncStorage from '@react-native-async-storage/async-storage'
import { createJSONStorage } from 'zustand/middleware'

/** Almacenamiento persistente de Zustand basado en AsyncStorage. */
export const zustandStorage = createJSONStorage(() => AsyncStorage)
