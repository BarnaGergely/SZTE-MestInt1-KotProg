# Algoritmus terv

Valamilyen speciális A* algoritmus fog kelleni.

## Alap működés

1. Kiszámolni egy ideális útvonalat kockáról kockára
    - A* algoritmus
    - Heurisztikák jelezzék az utat és a Coin-okat
2. Kiszámolni a gyorsulást az úton
3. Létre hozni az útvonal cella reprezentációját, lényegében egy feladat listát
4. Minden iterációban végre hajtani a soron következő lépést a listából

## Részletes A* algoritmus

### Heurisztikák

- tavolsagACeltol()
- legkozelebbiNemErintettCoinTavolsaga()
- eddigMegtettTavolsag()

### Algoritmus

```
A_Star(start,cel,heurisztikak){
   // min-kupac vagy prioritási sor, ami tárolja a felfedezett csúcsokat, amiket még szükséges elemezni
   // kezdetben csak a start van benne
   openSet += start;
   
   while openSet is not empty {
      current = the node in openSet having the lowest fScore[] value,
      if (current = cel) return reconstruct_path(cameFrom, current);
      
      openSet.Remove(current);
      for each neighbor of current {
         tentative_gScore := gScore[current] + d(current, neighbor)
         if (tentative_gScore < gScore[neighbor]) {
                    
            cameFrom[neighbor] = current; // Ez a szomszédba vezető út jobb minden eddiginél. Rögzítsük!
            gScore[neighbor] = tentative_gScore;
            fScore[neighbor] = gScore[neighbor] + h(neighbor);
            
            if (neighbor not in openSet) {
               openSet.add(neighbor);
            }
            
         }
      }
   }
   
   // A nyílt halmaz üres, de soha nem értük el a célcsúcsot
   return failure
}
```